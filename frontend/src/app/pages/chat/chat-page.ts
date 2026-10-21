import { DecimalPipe } from '@angular/common';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { ChatSource } from '../../core/api.models';
import { ApiService, errorMessage } from '../../core/api.service';
import { formatAnswer } from './format-answer';

interface Message {
  role: 'user' | 'assistant' | 'error';
  text: string;
  html?: string;
  sources?: ChatSource[];
}

@Component({
  selector: 'app-chat-page',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './chat-page.html',
  styleUrl: './chat-page.scss'
})
export class ChatPage {
  private readonly api = inject(ApiService);
  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');

  protected readonly messages = signal<Message[]>([]);
  protected readonly sending = signal(false);
  protected readonly openSource = signal<string | null>(null);
  protected question = '';

  protected readonly examples = [
    'Que doit faire le pilote en cas de panne radio ?',
    'Quelle réserve de carburant faut-il prévoir ?',
    'Quelle est la différence entre un METAR et un TAF ?'
  ];

  protected async send(text = this.question): Promise<void> {
    const question = text.trim();
    if (!question || this.sending()) {
      return;
    }
    this.question = '';
    this.messages.update(m => [...m, { role: 'user', text: question }]);
    this.sending.set(true);
    this.scrollToBottom();
    try {
      const response = await firstValueFrom(this.api.ask(question));
      this.messages.update(m => [...m, {
        role: 'assistant',
        text: response.answer,
        html: formatAnswer(response.answer),
        sources: response.sources
      }]);
    } catch (e) {
      this.messages.update(m => [...m, { role: 'error', text: errorMessage(e) }]);
    } finally {
      this.sending.set(false);
      this.scrollToBottom();
    }
  }

  protected onKeydown(event: KeyboardEvent): void {
    // Entrée envoie, Maj+Entrée fait un retour à la ligne
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  /** Un clic sur une pastille [n] dans la réponse ouvre la source correspondante. */
  protected onAnswerClick(event: MouseEvent, messageIndex: number): void {
    const number = (event.target as HTMLElement).closest<HTMLElement>('.cite')?.dataset['source'];
    if (number) {
      this.openSource.set(`${messageIndex}-${number}`);
    }
  }

  protected toggleSource(key: string): void {
    this.openSource.update(current => (current === key ? null : key));
  }

  protected clear(): void {
    this.messages.set([]);
    this.openSource.set(null);
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.scroller()?.nativeElement;
      el?.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
    });
  }
}
